import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { computed, inject, Signal } from '@angular/core';
import { FriendsService } from './friends.service';
import { firstValueFrom } from 'rxjs';
import { HelperService } from '../helper.service';
import { MethodsDictionary } from '@ngrx/signals/src/signal-store-models';
import { Friend } from './model'

type FriendsState = {
  friends: Friend[],
  selectedFriend: Friend | null
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
}

interface Methods extends MethodsDictionary {
  loadFriends(): Promise<void>;
  addFriend(friend: AddFriend): Promise<void>;
}

export const FriendsStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ friends }) => {
    const unSettledFriends = computed(() => {
      return friends().filter(friend => friend.loanAmount?.amount)
    });
    const finalAmount = computed(() => {
      return unSettledFriends().reduce((acc: number, friend: Friend) => {
        return friend.loanAmount?.isOwed ?
          acc + friend.loanAmount.amount :
          acc - friend.loanAmount!.amount;
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
  }))
);
