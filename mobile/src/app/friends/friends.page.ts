import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonButtons,
  IonIcon,
} from '@ionic/angular/standalone';
import { FriendsStore } from '../store/friends.store';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-friends',
  templateUrl: 'friends.page.html',
  styleUrls: ['friends.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons, IonIcon, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FriendsPage {
  readonly friendsStore = inject(FriendsStore);
  constructor() {
    this.friendsStore.loadFriends().then();
  }
}
