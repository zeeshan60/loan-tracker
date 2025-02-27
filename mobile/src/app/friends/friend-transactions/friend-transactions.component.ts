import { Component, inject, input, OnInit } from '@angular/core';
import { CurrencyPipe, JsonPipe } from '@angular/common';
import {
  IonAvatar, IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonItem,
  IonLabel, IonList, IonNav, IonSpinner, IonTitle, IonToolbar, ModalController,
} from '@ionic/angular/standalone';
import { NavParams } from '@ionic/angular';
import { Friend } from '../model';
import { TransactionDetailsComponent } from '../transaction-details/transaction-details.component';
import { FriendsStore } from '../friends.store';
import { HelperService } from '../../helper.service';
import { shortName } from '../../utility-functions';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { DefineExpenseComponent } from '../../define-expense/define-expense.component';

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
    JsonPipe,
  ],
})
export class FriendTransactionsComponent  implements OnInit {
  // readonly friend: Friend = inject(NavParams).data?.['friend'];
  readonly friend = input.required<Friend>();
  readonly nav = inject(IonNav);
  readonly friendsStore = inject(FriendsStore);
  readonly transactions = this.friendsStore.selectedTransactions;
  readonly modalCtrl = inject(ModalController);
  protected readonly shortName = shortName;

  constructor() {
  }

  ngOnInit() {
  }

  openTransactionDetails(transaction: any) {
    this.nav.push(TransactionDetailsComponent, {
      transaction,
      friend: this.friend()
    });
  }

  async addExpense() {
    const modal = await this.modalCtrl.create({
      component: DefineExpenseComponent,
      componentProps: { friend: this.friend() }
    })
    modal.present();
  }

  ionViewWillEnter() {
    this.friendsStore.setSelectedFriend(this.friend());
  }

  // ionViewWillLeave() {
  //   this.friendsStore.setSelectedFriend(null);
  // }
}
