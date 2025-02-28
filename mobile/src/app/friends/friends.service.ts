import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
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
      .pipe(
        map((response) => {
          // response.data.balance = {
          //   main: { currency: 'USD', isOwed: true, totalAmount: 2323 },
          //   other: [
          //     { currency: 'PKR', isOwed: true, totalAmount: 2323 },
          //     { currency: 'SGD', isOwed: false, totalAmount: 300 }
          //   ]
          // };
          return response;
        })
      );
  }

  addFriend(friend: AddFriend): Observable<any> {
    return this.http.post(PRIVATE_API + '/friends/add', friend)
  }
}
