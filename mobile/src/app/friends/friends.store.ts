import { patchState, signalStore, withComputed, withMethods, withState, WritableStateSource } from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { FriendsService } from './friends.service';
import { firstValueFrom } from 'rxjs';
import { HelperService } from '../helper.service';
import { FriendWithBalance, Transaction, TransactionsByMonth } from './model'
import { HttpClient } from '@angular/common/http';
import { PRIVATE_API } from '../constants';
import { LoadingController } from '@ionic/angular/standalone';
import { SplitOption, SplitOptionsEnum } from '../define-expense/define-expense.component';
import { StorageService } from '../services/storage.service';

export type AddFriend = {
  name: string,
  email: string|null,
  phoneNumber: string
}

export type OverallBalance = {
  main: Balance|null,
  other: OtherBalance[]
} | null;

export type Balance = {
  currency: string,
  amount: number,
  isOwed: boolean
}

export type OtherBalance = {
  convertedAmount: Balance,
  amount: Balance,
}

type FriendsState = {
  friends: FriendWithBalance[],
  loadingFriends: boolean,
  overallBalance: OverallBalance,
  selectedFriendBalance: {
    main: Balance,
    other: OtherBalance[]
  } | null,
  selectedFriendId: string | null,
  selectedTransactions: TransactionsByMonth[],
  mostlyUsedCurrencies: string[],
}

type AddUpdateExpenseFormValue = {
  description: string,
  currency: string,
  amount: number|null,
  type: SplitOption,
  transactionDate: string,
}

const initialState: FriendsState = {
  friends: [],
  loadingFriends: false,
  overallBalance: null,
  selectedFriendId: null,
  selectedTransactions: [],
  selectedFriendBalance: null,
  mostlyUsedCurrencies: [],
}

export const FriendsStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ friends, selectedFriendId, selectedTransactions }) => {
    const unSettledFriends = computed(() => friends().filter(friend => friend.mainBalance?.amount));
    const inActiveFriends = computed(() => friends().filter(friend => !friend.mainBalance?.amount)
      .sort((a, b) => a.name.localeCompare(b.name)));
    const selectedFriend = computed(() => friends().find(friend => friend.friendId === selectedFriendId()));
    const mostlyUsedSplitType = computed(() => {
      const map = new Map<SplitOption, number>();
      Object.keys(SplitOptionsEnum).forEach((splitType: string) => {
        if (!splitType.toLowerCase().includes('settle')) {
          map.set(splitType as SplitOption, 0);
        }
      })

      selectedTransactions()?.[0]?.transactions.slice(0, 3)
        .forEach((transaction) => {
          map.set(transaction.splitType, map.get(transaction.splitType) + 1)
        })

      return Array.from(map.entries()).sort((a, b) => {
        return a[1] > b[1] ? -1 : 1
      })[0][0] as SplitOption;
    })
    const mostlyUsedCurrency = computed(() => {
      const map = new Map<string, number>();

      selectedTransactions()?.[0]?.transactions.slice(0, 3)
        .forEach((transaction) => {
          map.set(transaction.amount.currency, (map.get(transaction.amount.currency) ?? 0) + 1)
        })

      return Array.from(map.entries()).sort((a, b) => {
        return a[1] > b[1] ? -1 : 1
      })[0]?.[0];
    })

    return {
      unSettledFriends,
      inActiveFriends,
      selectedFriend,
      mostlyUsedSplitType,
      mostlyUsedCurrency,
    }
  }),
  withMethods((
    store,
    friendsService = inject(FriendsService),
    helperService = inject(HelperService),
    storageService = inject(StorageService),
    http = inject(HttpClient),
    loadingCtrl = inject(LoadingController),
  ) => ({
    async loadFriends(config = { showLoader: true }): Promise<void> {
      try {
        const loadAllFriends = async () => {
          try {
            const {data} = await firstValueFrom(friendsService.loadAllFriends());
            patchState(store, { friends: data.friends, overallBalance: data.balance })
          } catch (e) {
            console.log(e);
          }
        };
        if (config.showLoader) {
          patchState(store, { loadingFriends: true });
          await helperService.withLoader(loadAllFriends);
          patchState(store, { loadingFriends: false });
        } else {
          await loadAllFriends();
        }
      } catch (e) {
        await helperService.showToast('Unable to load friends at the moment');
        patchState(store, { loadingFriends: false });
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
    async deleteFriend(friend: FriendWithBalance): Promise<boolean|undefined> {
      const confirmation = await helperService.showConfirmAlert(
        `You are about to delete ${friend.name} and ALL their associated transactions. This action cannot be undone. Do you wish to proceed?`, 'Let\'s do it'
      )
      if (confirmation.role !== 'confirm') return undefined;
      const loader = await loadingCtrl.create();
      await loader.present();
      try {
        await firstValueFrom(friendsService.deleteFriend(friend));
        await this.loadFriends();
        await helperService.showToast('Friend deleted successfully');
        return true;
      } catch (e: any) {
        await helperService.showToast(e.error?.error?.message || 'Unable to delete friend at the moment');
        throw e;
      } finally {
        await loader.dismiss();
      }
    },
    async setSelectedFriend(friendId: string|null) {
      function newFriendSelected() {
        return store.selectedFriendId() !== friendId
      }

      if (!newFriendSelected()) {
        return;
      }

      patchState(store, {
        selectedFriendId: friendId,
        selectedFriendBalance: null,
        selectedTransactions: []
      })

      if (friendId) {
        await this.loadSelectedTransactions()
      }
    },
    async loadSelectedTransactions() {
      if (!store.selectedFriendId()) {
        return;
      }
      try {
        patchState(store, { loadingFriends: true });
        await helperService.withLoader(async () => {
          const transactions = await firstValueFrom(http.get<{
            perMonth: TransactionsByMonth[],
            balance: OverallBalance
          }>(
            `${PRIVATE_API}/transactions/friend/byMonth`,
            {
              params: {
                friendId: store.selectedFriendId(),
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
      } finally {
        patchState(store, { loadingFriends: false });
      }
    },
    async deleteTransaction(transactionId: string): Promise<void> {
      let transactionAdded;
      try {
        await helperService.withLoader(async () => {
          transactionAdded = await firstValueFrom(http.delete(`${PRIVATE_API}/transactions/delete/transactionId/${transactionId}`))
        })
        await helperService.showToast('Transaction deleted successfully');
      } catch (e) {
        let errorMessage = 'Unable to delete transaction at the moment';
        await helperService.showToast(errorMessage);
        throw new Error(errorMessage)
      }
      if (transactionAdded) {
        this.loadSelectedTransactions()
        this.loadFriends()
      }
    },
    async settleUp(friend: FriendWithBalance, formValue: AddUpdateExpenseFormValue) {
      try {
        await helperService.withLoader(async () => {
          await this.addUpdateExpense(friend, formValue)
        })
      } catch (e) {
        helperService.showToast('Unable to settle up at the moment. Please try later');
        throw new Error('unable to settle at the moment');
      }
      await this.loadSelectedTransactions();
    },
    async addUpdateExpense(
      friend: FriendWithBalance,
      formValue: AddUpdateExpenseFormValue,
      transactionId?: string,
    ) {
      formValue.transactionDate = (new Date(formValue.transactionDate)).toISOString().split('.')[0] + 'Z';
      let transactionAddUpdateResponse;
      try {
        if (transactionId) {
          transactionAddUpdateResponse = await firstValueFrom(
            http.put<Transaction>(`${PRIVATE_API}/transactions/update/transactionId/${transactionId}`, {
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
        await helperService.showToast(`Unable to ${transactionId ? 'update': 'add'} expense at the moment`);
        throw e;
      }

      let postSaveActions = [this.loadFriends(), this.saveUsedCurrency(formValue.currency)];
      if (friend.friendId === store.selectedFriendId()) {
        postSaveActions.push(this.loadSelectedTransactions())
      }
      await Promise.all(postSaveActions);

      return transactionAddUpdateResponse;
    },
    async saveUsedCurrency(currency: string) {
      const previousCurrencies = (await storageService.getMostlyUsedCurrencies()) || [];
      if (!previousCurrencies.includes(currency)) {
        const mostlyUsedCurrencies = [...previousCurrencies, currency];
        await storageService.setMostlyUsedCurrencies(mostlyUsedCurrencies)
        patchState(store, { mostlyUsedCurrencies })
      }
    },
    async loadMostlyUsedCurrencies(): Promise<void> {
      patchState(store, { mostlyUsedCurrencies: (await storageService.getMostlyUsedCurrencies()) || [] })
    }
  }))
);
