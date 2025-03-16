import { inject, Injectable } from '@angular/core';
import { map, Observable, of } from 'rxjs';
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
    return of(
      {
        "data": {
          "friends": [
            {
              "photoUrl": null,
              "name": "Noman Tufail",
              "friendId": "580349b1-7870-4861-8edc-17b1f02f5851",
              "settled": false,
              "mainBalance": {
                "amount": 12528.18,
                "currency": "SGD",
                "isOwed": true
              },
              "otherBalances": [
                {
                  "amount": 116692,
                  "currency": "PKR",
                  "isOwed": true
                },
                {
                  "amount": 11975,
                  "currency": "SGD",
                  "isOwed": true
                }
              ]
            },
            {
              "photoUrl": null,
              "name": "Susral",
              "friendId": "d35b6e48-ea18-4aed-91a3-d927e5f3361e",
              "settled": false,
              "mainBalance": {
                "amount": 0.52,
                "currency": "SGD",
                "isOwed": true
              },
              "otherBalances": [
                {
                  "amount": 1000000,
                  "currency": "PKR",
                  "isOwed": true
                },
                {
                  "amount": 4740,
                  "currency": "SGD",
                  "isOwed": false
                }
              ]
            },
            {
              "photoUrl": null,
              "name": "Naveed",
              "friendId": "260da388-66ab-47ba-be29-338063f2bc62",
              "settled": false,
              "mainBalance": {
                "amount": 948.1,
                "currency": "SGD",
                "isOwed": true
              },
              "otherBalances": [
                {
                  "amount": 200000,
                  "currency": "PKR",
                  "isOwed": true
                }
              ]
            },
            {
              "photoUrl": null,
              "name": "Shah Faisal",
              "friendId": "4f5c90e6-95e6-4ad4-b4d1-05cb10466ba3",
              "settled": false,
              "mainBalance": {
                "amount": 36.94,
                "currency": "SGD",
                "isOwed": true
              },
              "otherBalances": [
                {
                  "amount": 7793,
                  "currency": "PKR",
                  "isOwed": true
                }
              ]
            },
            {
              "photoUrl": null,
              "name": "Aftab Akram",
              "friendId": "7acd2fcb-9fad-4e9c-80a1-e17c54b50258",
              "settled": false,
              "mainBalance": {
                "amount": 474.05,
                "currency": "SGD",
                "isOwed": true
              },
              "otherBalances": [
                {
                  "amount": 100000,
                  "currency": "PKR",
                  "isOwed": true
                }
              ]
            },
            {
              "photoUrl": null,
              "name": "zeeshan aflash",
              "friendId": "0e44279f-47dc-4e55-be2f-2975c1b7ab44",
              "settled": false,
              "mainBalance": null,
              "otherBalances": []
            },
            {
              "photoUrl": null,
              "name": "Shahid Rasool",
              "friendId": "3739d4c1-b934-418d-a7b4-6a5caceb76d0",
              "settled": false,
              "mainBalance": null,
              "otherBalances": []
            }
          ],
          "balance": {
            "main": {
              "amount": 13987.8,
              "currency": "SGD",
              "isOwed": true
            },
            "other": [
              {
                "amount": 1424485,
                "currency": "PKR",
                "isOwed": true
              },
              {
                "amount": 7235,
                "currency": "SGD",
                "isOwed": true
              }
            ]
          }
        },
        "next": null
      } as unknown as {
        data: {
          balance: {
            "main": Balance,
            "other": Balance[]
          }
          friends: FriendWithBalance[]
        },
      }
    );
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
}
