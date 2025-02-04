import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { inject } from '@angular/core';
import { FriendsService } from '../friends/friends.service';
import { firstValueFrom } from 'rxjs';

type FriendsState = {
  friends: any[],
  loading: boolean,
}

const initialState: FriendsState = {
  friends: [],
  loading: false,
}

export const FriendsStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((store, friendsService = inject(FriendsService)) => ({
    async loadFriends(): Promise<void> {
      patchState(store, { loading: true });
      const friends = await firstValueFrom(friendsService.loadAllFriends());
      patchState(store, { loading: false, friends })
    }
  }))
);
