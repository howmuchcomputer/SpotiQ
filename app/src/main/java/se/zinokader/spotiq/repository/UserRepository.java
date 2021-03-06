package se.zinokader.spotiq.repository;

import com.google.firebase.auth.FirebaseAuth;

import io.reactivex.Observable;

public class UserRepository {

    public UserRepository() {

    }

    public Observable<Boolean> logInFirebaseUser() {
        return Observable.create(subscriber -> Observable.just(FirebaseAuth.getInstance().signInAnonymously())
            .subscribe(authResultTask -> {
                authResultTask.addOnSuccessListener(success -> {
                    subscriber.onNext(true);
                    subscriber.onComplete();
                });
                authResultTask.addOnFailureListener(failure -> {
                    subscriber.onNext(false);
                    subscriber.onComplete();
                });

            }, throwable -> subscriber.onNext(false)));
    }
}
