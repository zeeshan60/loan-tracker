import { patchState, signalStore, withComputed, withMethods, withState, WritableStateSource } from '@ngrx/signals';
import { computed, inject, Signal } from '@angular/core';
import { FriendsService } from './friends.service';
import { firstValueFrom, map, Observable } from 'rxjs';
import { HelperService } from '../helper.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';
import { FriendWithBalance, Transaction, TransactionsByMonth } from './model'
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { PRIVATE_API } from '../constants';
import { LoadingController } from '@ionic/angular/standalone';
import { SplitOptions } from '../define-expense/define-expense.component';
import { help } from 'ionicons/icons';

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
  selectedFriendBalance: {
    main: Balance,
    other: Balance[]
  } | null,
  selectedFriend: FriendWithBalance | null,
  selectedTransactions: TransactionsByMonth[],
}

type AddUpdateExpenseFormValue = {
  description: string,
  currency: string,
  amount: number|null,
  type: SplitOptions,
  transactionDate: string,
}

const initialState: FriendsState = {
  friends: [],
  overallBalance: null,
  selectedFriend: null,
  selectedTransactions: [],
  selectedFriendBalance: null,
}

interface Methods extends MethodsDictionary {
  loadFriends(config?: { showLoader: boolean }): Promise<void>;
  addFriend(friend: AddFriend): Promise<FriendWithBalance>;
  updateFriend(friendId: string, friend: AddFriend): Promise<FriendWithBalance>;
  deleteFriend(friend: FriendWithBalance): Promise<void>;
  setSelectedFriend(friend: FriendWithBalance|null): Promise<void>;
  loadSelectedTransactions(): Promise<void>;
  deleteTransaction(transaction: Transaction): Promise<void>;
  settleUp(friend: FriendWithBalance, formValue: AddUpdateExpenseFormValue): Promise<void>;
  addUpdateExpense(friend: FriendWithBalance, formValue: AddUpdateExpenseFormValue, updatingTransaction?: Transaction): Promise<Transaction>;
}

export const FriendsStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ friends }) => {
    const unSettledFriends = computed(() => {
      return friends().filter(friend => friend.mainBalance?.amount)
    });
    const inActiveFriends = computed(() => {
      return friends().filter(friend => !friend.mainBalance?.amount).sort((a, b) => a.name.localeCompare(b.name));
    });
    return {
      unSettledFriends,
      inActiveFriends,
    }
  }),
  withMethods((
    store,
    friendsService = inject(FriendsService),
    helperService = inject(HelperService),
    http = inject(HttpClient),
    loadingCtrl = inject(LoadingController),
  ): Methods => ({
    async loadFriends(config = { showLoader: true }): Promise<void> {
      try {
        const loadAllFriends = async () => {
          const {data} = await firstValueFrom(friendsService.loadAllFriends());
          patchState(store, { friends: data.friends, overallBalance: data.balance })
        };
        if (config.showLoader) {
          await helperService.withLoader(loadAllFriends);
        } else {
          await loadAllFriends();
        }
      } catch (e) {
        await helperService.showToast('Unable to load friends at the moment');
      }
    },
    async addFriend(friend: AddFriend): Promise<FriendWithBalance> {
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
    async updateFriend(friendId: string, friend: AddFriend): Promise<FriendWithBalance> {
      try {
        const updatedFriend = await firstValueFrom(friendsService.updateFriend(friendId, friend));
        await this.loadFriends();
        await helperService.showToast('Friend updated successfully');
        return updatedFriend;
      } catch (e: any) {
        await helperService.showToast(e.error?.error?.message || 'Unable to update friend at the moment');
        throw e;
      }
    },
    async deleteFriend(friend: FriendWithBalance): Promise<void> {
      const confirmation = await helperService.showConfirmAlert(
        `Are you sure you want to remove all the transactions related to ${friend.name}.`, 'Let\'s do it'
      )
      if (confirmation.role !== 'confirm') return;
      const loader = await loadingCtrl.create();
      await loader.present();
      try {
        await firstValueFrom(friendsService.deleteFriend(friend));
        await this.loadFriends();
        await helperService.showToast('Friend deleted successfully');
      } catch (e: any) {
        await helperService.showToast(e.error?.error?.message || 'Unable to delete friend at the moment');
        throw e;
      } finally {
        await loader.dismiss();
      }
    },
    async setSelectedFriend(friend: FriendWithBalance|null) {
      if (store.selectedFriend()?.friendId !== friend?.friendId) {
        patchState(store, { selectedFriend: friend })
        if (friend) {
          await this.loadSelectedTransactions()
        }
      }
    },
    async loadSelectedTransactions() {
      if (!store.selectedFriend()) {
        return;
      }
      try {
        await helperService.withLoader(async () => {
          const transactions = await firstValueFrom(http.get<{
            perMonth: TransactionsByMonth[],
            balance: {
              "main": Balance,
              "other": Balance[]
            }
          }>(
            `${PRIVATE_API}/transactions/friend/byMonth`,
            {
              params: {
                friendId: store.selectedFriend()?.friendId!,
                timeZone: helperService.getTimeZone()
              }
            }
          ))
          patchState(store, {
            selectedTransactions: transactions.perMonth,
            selectedFriendBalance: transactions.balance,
          })
        })
      } catch (e: any) {
        await helperService.showToast(e.toString())
      }
    },
    async deleteTransaction(transaction: Transaction): Promise<void> {
      let transactionAdded;
      try {
        await helperService.withLoader(async () => {
          transactionAdded = await firstValueFrom(http.delete(`${PRIVATE_API}/transactions/delete/transactionId/${transaction.transactionId}`))
        })
        await helperService.showToast('Transaction deleted successfully');
      } catch (e) {
        await helperService.showToast('Unable to add friend at the moment');
        throw new Error('Unable to add friend at the moment')
      }
      if (transactionAdded) {
        this.loadSelectedTransactions()
      }
    },
    async settleUp(friend: FriendWithBalance, formValue: AddUpdateExpenseFormValue) {
      const confirmation = await helperService.showConfirmAlert(
        `You are going to settle up everything with ${friend.name}.`, 'Let\'s do it'
      )
      if (confirmation.role !== 'confirm') return;
      try {
        await helperService.withLoader(async () => {
          await this.addUpdateExpense(friend, formValue)
        })
      } catch (e) {
        helperService.showToast('Unable to settle up at the moment. Please try later');
      }
      await this.loadSelectedTransactions();
    },
    async addUpdateExpense(
      friend: FriendWithBalance,
      formValue: AddUpdateExpenseFormValue,
      updatingTransaction?: Transaction,
    ) {
      formValue.transactionDate = (new Date(formValue.transactionDate)).toISOString();

      let transactionAddUpdateResponse;
      try {
        if (updatingTransaction) {
          transactionAddUpdateResponse = await firstValueFrom(
            http.put<Transaction>(`${PRIVATE_API}/transactions/update/transactionId/${updatingTransaction.transactionId}`, {
              ...formValue,
            })
          );
        } else {
          transactionAddUpdateResponse = await firstValueFrom(http.post<Transaction>(`${PRIVATE_API}/transactions/add`, {
            ...formValue,
            recipientId: friend.friendId
          }));
        }
      } catch (e) {
        await helperService.showToast('Unable to add/update expense at the moment');
        throw e;
      }

      let postSaveActions = [this.loadFriends()];
      if (
        store.selectedFriend()
        && friend.friendId === store.selectedFriend()?.friendId
      ) {
        postSaveActions.push(this.loadSelectedTransactions())
      }
      await Promise.all(postSaveActions);

      return transactionAddUpdateResponse;
    }
  }))
);
