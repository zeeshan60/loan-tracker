import { patchState, signalStore, withComputed, withMethods, withState, WritableStateSource } from '@ngrx/signals';
import { computed, inject, Signal } from '@angular/core';
import { FriendsService } from './friends.service';
import { firstValueFrom, map, Observable } from 'rxjs';
import { HelperService } from '../helper.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';
import { FriendWithBalance, Transaction, TransactionsByMonth } from './model'
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { PRIVATE_API } from '../constants';
import { AlertController } from '@ionic/angular/standalone';

export type AddFriend = {
  name: string,
  email: string|null,
  phoneNumber: string
}

type Balance = {
  currency: string,
  amount: number,
  isOwed: boolean
}

type FriendsState = {
  friends: FriendWithBalance[],
  overallBalance: {
    main: Balance,
    other: Balance[]
  } | null,
  selectedFriend: FriendWithBalance | null,
  selectedTransactions: TransactionsByMonth[],
  loading: boolean,
}

const initialState: FriendsState = {
  friends: [],
  overallBalance: null,
  loading: false,
  selectedFriend: null,
  selectedTransactions: []
}

interface Methods extends MethodsDictionary {
  loadFriends(config?: { showLoader: boolean }): Promise<void>;
  addFriend(friend: AddFriend): Promise<void>;
  setSelectedFriend(friend: FriendWithBalance|null): void;
  loadSelectedTransactions(): Promise<void>;
  deleteTransaction(transaction: Transaction): Promise<void>;
  setLoading(isLoading: boolean): void;
  settleUp(friend: FriendWithBalance): Promise<void>;
}

export const FriendsStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ friends }) => {
    const unSettledFriends = computed(() => {
      return friends().filter(friend => friend.mainBalance?.amount)
    });
    return {
      unSettledFriends,
    }
  }),
  withMethods((
    store,
    friendsService = inject(FriendsService),
    helperService = inject(HelperService),
    http = inject(HttpClient),
  ): Methods => ({
    async loadFriends(config = { showLoader: true }): Promise<void> {
      if (config.showLoader) {
        patchState(store, { loading: true });
      }
      try {
        const {data} = await firstValueFrom(friendsService.loadAllFriends());
        patchState(store, { friends: data.friends, overallBalance: data.balance })
      } catch (e) {
        await helperService.showToast('Unable to load friends at the moment');
      } finally {
        if (config.showLoader) {
          patchState(store, { loading: false })
        }
      }
    },
    async addFriend(friend: AddFriend): Promise<void> {
      try {
        const addedFriend = await firstValueFrom(friendsService.addFriend(friend));
        await this.loadFriends();
        helperService.showToast('Friend created successfully');
        return addedFriend;
      } catch (e: any) {
        helperService.showToast(e.error?.error?.message || 'Unable to add friend at the moment');
        throw e;
      }
    },
    async setSelectedFriend(friend: FriendWithBalance|null) {
      if (store.selectedFriend()?.friendId !== friend?.friendId) {
        patchState(store, { selectedFriend: friend })
        if (friend) {
          this.loadSelectedTransactions()
        }
      }
    },
    async loadSelectedTransactions() {
      if (!store.selectedFriend()) {
        return;
      }
      try {
        patchState(store, { loading: true })
        const transactions = await firstValueFrom(http.get<{ perMonth: TransactionsByMonth[]}>(
          `${PRIVATE_API}/transactions/friend/byMonth`,
          {
            params: {
              friendId: store.selectedFriend()?.friendId!,
              timeZone: helperService.getTimeZone()
            }
          }
        ) as Observable<{ perMonth: TransactionsByMonth[] }>)
        patchState(store, { selectedTransactions: transactions.perMonth, loading: false })
      } catch (e: any) {
        patchState(store, { loading: false })
        helperService.showToast(e.toString())
      }
    },
    async deleteTransaction(transaction: Transaction): Promise<void> {
      try {
        patchState(store, { loading: true })
        await firstValueFrom(http.delete(`${PRIVATE_API}/transactions/delete/transactionId/${transaction.transactionId}`))
        this.loadSelectedTransactions()
        helperService.showToast('Transaction deleted successfully');
      } catch (e) {
        helperService.showToast('Unable to add friend at the moment');
        throw new Error('Unable to add friend at the moment')
      } finally {
        patchState(store, { loading: false })
      }
    },
    setLoading(isLoading: boolean) {
      patchState(store, { loading: isLoading })
    },
    async settleUp(friend: FriendWithBalance) {
      const confirmation = await helperService.showConfirmAlert(
        `You are going to settle up everything with ${friend.name}.`, 'Let\'s do it'
      )
      if (confirmation.role !== 'confirm') return;

      try {
        patchState(store, {loading: true});
        await firstValueFrom(http.put(`${PRIVATE_API}/friends/settle/${friend.friendId}`, {}));
      } catch (e) {
        helperService.showToast('Unable to settle up at the moment. Please try later');
      } finally {
        patchState(store, { loading: false })
      }
      await this.loadSelectedTransactions();
    }
  }))
);
