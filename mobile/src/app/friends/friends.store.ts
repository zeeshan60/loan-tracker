import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { computed, inject, Signal } from '@angular/core';
import { FriendsService } from './friends.service';
import { firstValueFrom, Observable } from 'rxjs';
import { HelperService } from '../helper.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';
import { Friend, TransactionsByMonth } from './model'
import { HttpClient } from '@angular/common/http';
import { PRIVATE_API } from '../constants';

type FriendsState = {
  friends: Friend[],
  selectedFriend: Friend | null,
  selectedTransactions: TransactionsByMonth[],
  loading: boolean,
}

export type AddFriend = {
  name: string,
  email: string|null,
  phoneNumber: string
}

const initialState: FriendsState = {
  friends: [],
  loading: false,
  selectedFriend: null,
  selectedTransactions: []
}

interface Methods extends MethodsDictionary {
  loadFriends(): Promise<void>;
  addFriend(friend: AddFriend): Promise<void>;
  setSelectedFriend(friend: Friend|null): void;
  loadSelectedTransactions(): Promise<void>;
}

export const FriendsStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ friends }) => {
    const unSettledFriends = computed(() => {
      return friends().filter(friend => friend.mainBalance?.amount)
    });
    const finalAmount = computed(() => {
      return unSettledFriends().reduce((acc: number, friend: Friend) => {
        return friend.mainBalance?.isOwed ?
          acc + friend.mainBalance.amount :
          acc - friend.mainBalance!.amount;
      }, 0);
    });
    return {
      unSettledFriends,
      finalAmount,
    }
  }),
  withMethods((
    store,
    friendsService = inject(FriendsService),
    helperService = inject(HelperService),
    http = inject(HttpClient),
  ): Methods => ({
    async loadFriends(): Promise<void> {
      patchState(store, { loading: true });
      try {
        const {data} = await firstValueFrom(friendsService.loadAllFriends());
        patchState(store, { friends: data.friends })
      } catch (e) {
        await helperService.showToast('Unable to load friends at the moment');
      } finally {
        patchState(store, { loading: false })
      }
    },
    async addFriend(friend: AddFriend): Promise<void> {
      await firstValueFrom(friendsService.addFriend(friend));
    },
    setSelectedFriend(friend: Friend|null) {
      let updatedState: {[key: string]: any} = { selectedFriend: friend }
      if (!friend) {
        updatedState['selectedTransactions'] = [];
      }
      patchState(store, { ...updatedState })
    },
    async loadSelectedTransactions() {
      if (!store.selectedFriend()) {
       throw new Error('No friend selected');
      }
      try {
        patchState(store, { loading: true })
        const transactions = await firstValueFrom(http.get(
          `${PRIVATE_API}/transactions/friend/byMonth`,
          {
            params: {
              friendId: store.selectedFriend()?.friendId!,
              timeZone: helperService.getTimeZone()
            }
          }
        ) as Observable<{ perMonth: any[] }>)
        patchState(store, { selectedTransactions: transactions.perMonth, loading: false })
      } catch (e) {
        patchState(store, { loading: false })
      }
    }
  }))
);
