import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { inject } from '@angular/core';
import { FriendsService } from './friends.service';
import { firstValueFrom } from 'rxjs';
import { HelperService } from '../helper.service';
import { HttpErrorResponse } from '@angular/common/http';

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
  withMethods((
    store,
    friendsService = inject(FriendsService),
    helperService = inject(HelperService),
  ) => ({
    async loadFriends(): Promise<void> {
      patchState(store, { loading: true });
      try {
        const friends = await firstValueFrom(friendsService.loadAllFriends());
        patchState(store, { friends })
      } catch (e) {
        await helperService.showToast('Unable to load friends from the moment');
      } finally {
        patchState(store, { loading: false })
      }
    },
  }))
);
