import { inject, Injectable } from '@angular/core';
import { map, Observable, of, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { AddFriend } from './friends.store';
import { FriendWithBalance } from './model';
import { PRIVATE_API } from '../constants';

interface Balance {
  "currency": string;
  "amount": number;
  "isOwed": boolean;
}

@Injectable({
  providedIn: 'root'
})
export class FriendsService {
  readonly http = inject(HttpClient);

  constructor() { }

  loadAllFriends(): Observable<{
    data: {
      balance: {
        "main": Balance,
        "other": Balance[]
      }
      friends: FriendWithBalance[]
    },
  }> {
    return this.http.get<{
      data: {
        balance: {
          "main": Balance,
          "other": Balance[]
        }
        friends: FriendWithBalance[]
      }
    }>(PRIVATE_API + '/friends')
  }

  addFriend(friend: AddFriend): Observable<any> {
    return this.http.post(PRIVATE_API + '/friends/add', friend)
  }

  updateFriend(friend: AddFriend): Observable<FriendWithBalance> {
    return of({
        "friendId": "12345",
        "photoUrl": "https://example.com/photos/john.jpg",
        "name": "John Doe",
        "phoneNumber": "+1234567890",
        "email": "john.doe@example.com",
        "mainBalance": {
          "amount": 50.75,
          "currency": "USD",
          "isOwed": true
        },
        "otherBalances": [
          {
            "amount": 10,
            "currency": "PKR",
            "isOwed": false
          },
          {
            "amount": 100,
            "currency": "USD",
            "isOwed": true
          }
        ]
      }
    );
    // return this.http.put(PRIVATE_API + '/friends/add', friend)
  }

  deleteFriend(friend: FriendWithBalance) {
    return timer(1000).pipe(map((response) => true));
    // return this.http.delete(PRIVATE_API + '/friends/delete')
  }
}
