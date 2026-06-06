package com.gather.service;

import com.gather.exception.InvalidTokenException;
import com.gather.exception.UnknownCityException;
import com.gather.model.dto.response.LocationResponse;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock FirebaseAuth firebaseAuth;
    @Mock Firestore firestore;
    @Mock CollectionReference users;
    @Mock DocumentReference userDoc;

    private UserService userService;

    @BeforeEach
    void setUp() {
        // CityRegistry has no dependencies; use the real one so we exercise the real city list.
        userService = new UserService(firebaseAuth, firestore, new CityRegistry());
    }

    private void stubVerifiedToken(String uid) throws FirebaseAuthException {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateLocationPersistsAndReturnsCity() throws Exception {
        stubVerifiedToken("uid123");
        when(firestore.collection("users")).thenReturn(users);
        when(users.document("uid123")).thenReturn(userDoc);
        ApiFuture<WriteResult> future = mock(ApiFuture.class);
        when(userDoc.update(anyMap())).thenReturn(future);
        when(future.get()).thenReturn(mock(WriteResult.class));

        LocationResponse response = userService.updateLocation("id-token", "seattle");

        assertThat(response.getCityId()).isEqualTo("seattle");
        assertThat(response.getCityName()).isEqualTo("Seattle");
    }

    @Test
    void updateLocationRejectsUnknownCity() throws Exception {
        stubVerifiedToken("uid123");

        assertThatThrownBy(() -> userService.updateLocation("id-token", "atlantis"))
                .isInstanceOf(UnknownCityException.class);
    }

    @Test
    void updateLocationThrowsInvalidTokenOnBadToken() throws Exception {
        when(firebaseAuth.verifyIdToken("bad-token", true)).thenThrow(mock(FirebaseAuthException.class));

        assertThatThrownBy(() -> userService.updateLocation("bad-token", "seattle"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
