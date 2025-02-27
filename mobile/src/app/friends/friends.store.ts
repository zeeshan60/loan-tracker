import { patchState, signalStore, withComputed, withMethods, withState, WritableStateSource } from '@ngrx/signals';
import { computed, inject, Signal } from '@angular/core';
import { FriendsService } from './friends.service';
import { firstValueFrom, map, Observable } from 'rxjs';
import { HelperService } from '../helper.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';
import { Friend, Transaction, TransactionsByMonth } from './model'
import { HttpClient } from '@angular/common/http';
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
  friends: Friend[],
  overallBalance: {
    main: Balance,
    other: Balance[]
  } | null,
  selectedFriend: Friend | null,
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
  loadFriends(): Promise<void>;
  addFriend(friend: AddFriend): Promise<void>;
  setSelectedFriend(friend: Friend|null): void;
  loadSelectedTransactions(): Promise<void>;
  deleteTransaction(transaction: Transaction): Promise<void>;
}

async function loadSelectedTransactions(
  store: WritableStateSource<FriendsState>,
  http: HttpClient,
  helperService: HelperService,
  selectedFriend: Friend|null,
  ) {
  if (!selectedFriend) {
    throw new Error('No friend selected');
  }
  try {
    patchState(store, { loading: true })
    const transactions = await firstValueFrom(http.get<{ perMonth: TransactionsByMonth[]}>(
      `${PRIVATE_API}/transactions/friend/byMonth`,
      {
        params: {
          friendId: selectedFriend?.friendId!,
          timeZone: helperService.getTimeZone()
        }
      }
    ) as Observable<{ perMonth: TransactionsByMonth[] }>)
    patchState(store, { selectedTransactions: transactions.perMonth, loading: false })
  } catch (e: any) {
    patchState(store, { loading: false })
    helperService.showToast(e.toString())
  }
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
    alertCtrl = inject(AlertController),
  ): Methods => ({
    async loadFriends(): Promise<void> {
      patchState(store, { loading: true });
      try {
        const {data} = await firstValueFrom(friendsService.loadAllFriends());
        patchState(store, { friends: data.friends, overallBalance: data.balance })
      } catch (e) {
        await helperService.showToast('Unable to load friends at the moment');
      } finally {
        patchState(store, { loading: false })
      }
    },
    async addFriend(friend: AddFriend): Promise<void> {
      try {
        await firstValueFrom(friendsService.addFriend(friend));
        helperService.showToast('Friend created successfully');
      } catch (e) {
        helperService.showToast('Unable to add friend at the moment');
      }
    },
    async setSelectedFriend(friend: Friend|null) {
      if (store.selectedFriend()?.friendId !== friend?.friendId) {
        patchState(store, { selectedFriend: friend })
        if (friend) {
          await loadSelectedTransactions(store, http, helperService, store.selectedFriend());
        }
      }
    },
    async loadSelectedTransactions() {
      await loadSelectedTransactions(store, http, helperService, store.selectedFriend())
    },
    async deleteTransaction(transaction: Transaction): Promise<void> {
      try {
        const response = await helperService.showConfirmAlert()
        if (response.role === 'confirm') {
          patchState(store, { loading: true })
          await firstValueFrom(http.delete(`${PRIVATE_API}/transactions/delete/transactionId/${transaction.transactionId}`))
          // todo: load active transactions
          helperService.showToast('Transaction deleted successfully');
        }
      } catch (e) {
        helperService.showToast('Unable to add friend at the moment');
      } finally {
        patchState(store, { loading: false })
      }
    },
  }))
);
