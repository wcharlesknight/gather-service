package com.gather.service;

import com.gather.exception.InvalidTokenException;
import com.gather.model.dto.request.RegisterRequest;
import com.gather.model.dto.response.AuthResponse;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock FirebaseAuth firebaseAuth;
    @Mock Firestore firestore;
    @Mock CollectionReference users;
    @Mock DocumentReference userDoc;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(firebaseAuth, firestore);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com");
        req.setPassword("hunter2");
        req.setDisplayName("Alice");
        return req;
    }

    @SuppressWarnings("unchecked")
    private void stubFirestoreUsersDoc(String uid) {
        when(firestore.collection("users")).thenReturn(users);
        when(users.document(uid)).thenReturn(userDoc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registerReturnsTokenOnSuccess() throws Exception {
        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(firebaseAuth.createUser(any())).thenReturn(userRecord);
        when(firebaseAuth.createCustomToken("uid123")).thenReturn("custom-token");

        stubFirestoreUsersDoc("uid123");
        ApiFuture<WriteResult> future = mock(ApiFuture.class);
        when(userDoc.set(any())).thenReturn(future);
        when(future.get()).thenReturn(mock(WriteResult.class));

        AuthResponse response = authService.register(registerRequest());

        assertThat(response.getUid()).isEqualTo("uid123");
        assertThat(response.getCustomToken()).isEqualTo("custom-token");
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void registerTranslatesEmailAlreadyExists() throws Exception {
        FirebaseAuthException ex = mock(FirebaseAuthException.class);
        when(ex.getAuthErrorCode()).thenReturn(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        when(firebaseAuth.createUser(any())).thenThrow(ex);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(AuthService.EmailAlreadyExistsException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registerRollsBackAuthUserWhenProfileWriteFails() throws Exception {
        UserRecord userRecord = mock(UserRecord.class);
        when(userRecord.getUid()).thenReturn("uid123");
        when(firebaseAuth.createUser(any())).thenReturn(userRecord);

        stubFirestoreUsersDoc("uid123");
        ApiFuture<WriteResult> future = mock(ApiFuture.class);
        when(userDoc.set(any())).thenReturn(future);
        when(future.get()).thenThrow(new ExecutionException(new RuntimeException("firestore down")));

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(RuntimeException.class);

        // H4: the orphaned Auth user must be deleted so the email is freed.
        verify(firebaseAuth).deleteUser("uid123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginRecordsTimestampAndReturnsProfile() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid123");
        when(token.getClaims()).thenReturn(Map.of("name", "Alice"));
        when(token.getEmail()).thenReturn("alice@example.com");
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);

        stubFirestoreUsersDoc("uid123");
        ApiFuture<WriteResult> future = mock(ApiFuture.class);
        when(userDoc.update(anyMap())).thenReturn(future);
        when(future.get()).thenReturn(mock(WriteResult.class));

        AuthResponse response = authService.login("id-token");

        assertThat(response.getUid()).isEqualTo("uid123");
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void loginThrowsInvalidTokenOnBadToken() throws Exception {
        when(firebaseAuth.verifyIdToken("bad-token", true)).thenThrow(mock(FirebaseAuthException.class));

        assertThatThrownBy(() -> authService.login("bad-token"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
