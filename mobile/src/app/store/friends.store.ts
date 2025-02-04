import { signalStore, withState } from '@ngrx/signals';

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
  withState(initialState)
);
