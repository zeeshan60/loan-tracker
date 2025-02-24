import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import {
  IonAvatar, IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel, IonList, IonNav, IonSpinner, IonTitle, IonToolbar,
} from '@ionic/angular/standalone';
import { NavParams } from '@ionic/angular';
import { Friend } from '../model';
import { TransactionDetailsComponent } from '../transaction-details/transaction-details.component';
import { FriendsStore } from '../friends.store';

@Component({
  selector: 'app-friend-transactions',
  templateUrl: './friend-transactions.component.html',
  styleUrls: ['./friend-transactions.component.scss'],
  standalone: true,
  imports: [
    CurrencyPipe,
    IonAvatar,
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonItem,
    IonLabel,
    IonList,
    IonTitle,
    IonToolbar,
    IonBackButton,
    IonSpinner,
  ],
})
export class FriendTransactionsComponent  implements OnInit {
  readonly friend: Friend = inject(NavParams).data?.['friend'];
  readonly nav = inject(IonNav);
  readonly friendsStore = inject(FriendsStore);
  readonly transactionDetails = TransactionDetailsComponent;
  readonly transactions = [
    { name: 'bablu ko diye' },
    { name: 'mom ka kharcha' },
  ]

  constructor() {
  }

  ngOnInit() {}

  openTransactionDetails(transaction: any) {
    this.nav.push(this.transactionDetails, {
      transaction
    });
  }

  ionViewDidEnter() {
    this.friendsStore.setSelectedFriend(this.friend || null);
  }

  ionViewDidLeave() {
    this.friendsStore.setSelectedFriend(null);
  }
}
