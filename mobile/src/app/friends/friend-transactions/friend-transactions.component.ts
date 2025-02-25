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
import { HelperService } from '../../helper.service';
import { shortName } from '../../utility-functions';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DateFormatPipe } from '../../pipes/date-format.pipe';

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
    ShortenNamePipe,
    DateFormatPipe,
  ],
})
export class FriendTransactionsComponent  implements OnInit {
  readonly friend: Friend = inject(NavParams).data?.['friend'];
  readonly nav = inject(IonNav);
  readonly friendsStore = inject(FriendsStore);
  readonly transactionDetails = TransactionDetailsComponent;
  readonly transactions = this.friendsStore.selectedTransactions;
  protected readonly shortName = shortName;

  constructor() {
  }

  ngOnInit() {}

  openTransactionDetails(transaction: any) {
    this.nav.push(this.transactionDetails, {
      transaction
    });
  }

  ionViewWillEnter() {
    this.friendsStore.setSelectedFriend(this.friend || null);
    this.friendsStore.loadSelectedTransactions();
  }

  ionViewWillLeave() {
    this.friendsStore.setSelectedFriend(null);
  }
}
