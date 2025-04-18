import { inject, Injectable } from '@angular/core';
import { map, Observable, timer } from 'rxjs';
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

  updateFriend(friendId: string, friend: AddFriend): Observable<FriendWithBalance> {
    return this.http.put<FriendWithBalance>(PRIVATE_API + `/friends/${friendId}`, friend)
  }

  deleteFriend(friend: FriendWithBalance) {
    return timer(1000).pipe(map((response) => true));
    // return this.http.delete(PRIVATE_API + '/friends/delete')
  }
}
