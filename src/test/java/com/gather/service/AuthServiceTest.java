package com.gather.service;

import com.gather.exception.InvalidTokenException;
import com.gather.model.dto.request.RegisterRequest;
import com.gather.model.dto.response.AuthResponse;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
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
        when(future.get(anyLong(), any())).thenReturn(mock(WriteResult.class));

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
        when(future.get(anyLong(), any())).thenThrow(new ExecutionException(new RuntimeException("firestore down")));

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(RuntimeException.class);

        // H4: the orphaned Auth user must be deleted so the email is freed.
        verify(firebaseAuth).deleteUser("uid123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginRecordsTimestampForExistingUser() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid123");
        when(token.getClaims()).thenReturn(Map.of("name", "Alice"));
        when(token.getEmail()).thenReturn("alice@example.com");
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);

        stubFirestoreUsersDoc("uid123");
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.exists()).thenReturn(true);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        when(userDoc.get()).thenReturn(getFuture);
        when(getFuture.get(anyLong(), any())).thenReturn(snapshot);
        ApiFuture<WriteResult> setFuture = mock(ApiFuture.class);
        when(userDoc.set(anyMap(), any(SetOptions.class))).thenReturn(setFuture);
        when(setFuture.get(anyLong(), any())).thenReturn(mock(WriteResult.class));

        AuthResponse response = authService.login("id-token");

        assertThat(response.getUid()).isEqualTo("uid123");
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.isNewUser()).isFalse();
        // Existing profile: only lastLoginAt is merged, not the create-time fields.
        verify(userDoc).set(argThat((Map<String, Object> m) ->
                m.containsKey("lastLoginAt") && !m.containsKey("createdAt")), any(SetOptions.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginProvisionsProfileForNewSocialUser() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid456");
        when(token.getClaims()).thenReturn(Map.of(
                "name", "Bob",
                "firebase", Map.of("sign_in_provider", "google.com")));
        when(token.getEmail()).thenReturn("bob@example.com");
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);

        stubFirestoreUsersDoc("uid456");
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.exists()).thenReturn(false);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        when(userDoc.get()).thenReturn(getFuture);
        when(getFuture.get(anyLong(), any())).thenReturn(snapshot);
        ApiFuture<WriteResult> setFuture = mock(ApiFuture.class);
        when(userDoc.set(anyMap(), any(SetOptions.class))).thenReturn(setFuture);
        when(setFuture.get(anyLong(), any())).thenReturn(mock(WriteResult.class));

        AuthResponse response = authService.login("id-token");

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.getDisplayName()).isEqualTo("Bob");
        // First login provisions the full profile, including provider and onboarding defaults.
        verify(userDoc).set(argThat((Map<String, Object> m) ->
                "google.com".equals(m.get("provider"))
                        && Boolean.FALSE.equals(m.get("hasCompletedOnboarding"))
                        && m.containsKey("createdAt")
                        && m.containsKey("lastLoginAt")), any(SetOptions.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginProvisionsProfileForAppleUser() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid789");
        when(token.getClaims()).thenReturn(Map.of(
                "name", "Charlie",
                "firebase", Map.of("sign_in_provider", "apple.com")));
        when(token.getEmail()).thenReturn("charlie@example.com");
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);

        stubFirestoreUsersDoc("uid789");
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.exists()).thenReturn(false);
        ApiFuture<DocumentSnapshot> getFuture = mock(ApiFuture.class);
        when(userDoc.get()).thenReturn(getFuture);
        when(getFuture.get(anyLong(), any())).thenReturn(snapshot);
        ApiFuture<WriteResult> setFuture = mock(ApiFuture.class);
        when(userDoc.set(anyMap(), any(SetOptions.class))).thenReturn(setFuture);
        when(setFuture.get(anyLong(), any())).thenReturn(mock(WriteResult.class));

        AuthResponse response = authService.login("id-token");

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.getDisplayName()).isEqualTo("Charlie");
        assertThat(response.getEmail()).isEqualTo("charlie@example.com");
        // Apple sign-in also provisions the full profile with the apple.com provider.
        verify(userDoc).set(argThat((Map<String, Object> m) ->
                "apple.com".equals(m.get("provider"))
                        && Boolean.FALSE.equals(m.get("hasCompletedOnboarding"))
                        && m.containsKey("createdAt")
                        && m.containsKey("lastLoginAt")), any(SetOptions.class));
    }

    @Test
    void loginThrowsInvalidTokenOnBadToken() throws Exception {
        when(firebaseAuth.verifyIdToken("bad-token", true)).thenThrow(mock(FirebaseAuthException.class));

        assertThatThrownBy(() -> authService.login("bad-token"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
