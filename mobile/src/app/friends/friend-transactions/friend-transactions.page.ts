import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonButtons,
  IonIcon, IonList, IonItem, IonAvatar, IonLabel,
} from '@ionic/angular/standalone';
import { FriendsStore } from '../friends.store';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-friends',
  templateUrl: 'friends.page.html',
  styleUrls: ['friends.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons, IonIcon, FormsModule, IonList, IonItem, IonAvatar, IonLabel, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FriendTransactionsPage {
  readonly friendsStore = inject(FriendsStore);
  constructor() {}
}
