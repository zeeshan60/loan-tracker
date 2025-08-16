import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { HelperService } from '../helper.service';
import { HttpClient } from '@angular/common/http';
import { LoadingController } from '@ionic/angular/standalone';
import { StorageService } from '../services/storage.service';
import { Group, GroupWithBalance } from './model';

type GroupsState = {
  groups: GroupWithBalance[]
  loadingGroups: boolean,
}

const initialState: GroupsState = {
  groups: [],
  loadingGroups: false,
}

export const GroupsStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ groups }) => {
    let unSettledGroups =  computed(() => groups);
    let inActiveGroups =  computed(() => [] as Group[]);
    return {
      unSettledGroups,
      inActiveGroups
    }
  }),
  withMethods((
    store,
    helperService = inject(HelperService),
    storageService = inject(StorageService),
    http = inject(HttpClient),
    loadingCtrl = inject(LoadingController),
  ) => ({
    async loadMostlyUsedCurrencies(): Promise<void> {

    }
  }))
);
