import { Component, inject, input, OnInit } from '@angular/core';
import {
  AlertController,
  IonAvatar,
  IonBackButton, IonButton,
  IonButtons,
  IonContent,
  IonHeader, IonIcon, IonItem, IonLabel,
  IonList, IonNav,
  IonTitle,
  IonToolbar, ModalController,
} from '@ionic/angular/standalone';
import { Friend, HistoryChangeType, Transaction } from '../model';
import { NavParams } from '@ionic/angular';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { DefineExpenseComponent, SplitOptions } from '../../define-expense/define-expense.component';
import { FriendsStore } from '../friends.store';
import { HelperService } from '../../helper.service';

const historyChangeType = {
  [HistoryChangeType.DESCRIPTION]: "description",
  [HistoryChangeType.TOTAL_AMOUNT]: "total amount",
  [HistoryChangeType.CURRENCY]: "currency",
  [HistoryChangeType.SPLIT_TYPE]: "split type",
  [HistoryChangeType.DELETED]: "deleted",
  [HistoryChangeType.TRANSACTION_DATE]: "transaction date"
}
@Component({
    selector: 'app-transaction-details',
    templateUrl: './transaction-details.component.html',
    styleUrls: ['./transaction-details.component.scss'],
    standalone: true,
  imports: [
    IonBackButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonList,
    IonTitle,
    IonToolbar,
    IonLabel,
    CurrencyPipe,
    DateFormatPipe,
    IonAvatar,
    IonIcon,
    IonItem,
    ShortenNamePipe,
    IonButton,
    DatePipe,
  ],
})
export class TransactionDetailsComponent  implements OnInit {
  readonly SplitOptions = SplitOptions;
  readonly friendsStore = inject(FriendsStore);
  readonly helperService = inject(HelperService);
  readonly modalCtrl = inject(ModalController);
  readonly nav = inject(IonNav);
  readonly transaction = input.required<Transaction>();
  readonly friend = input.required<Friend>();
  constructor() {
  }

  ngOnInit() {}

  async deleteTransaction() {
    const response = await this.helperService.showConfirmAlert()
    if (response.role === 'confirm') {
      try {
        await this.friendsStore.deleteTransaction(this.transaction());
        this.nav.pop();
      } catch (e) {}
    }
  }

  async updateTransaction() {
    const modal = await this.modalCtrl.create({
      component: DefineExpenseComponent,
      componentProps: {
        friend: this.friend(),
        isUpdating: true,
        transaction: this.transaction(),
      }
    })
    modal.present();
  }

  protected readonly historyChangeType = historyChangeType;
  protected readonly HistoryChangeType = HistoryChangeType;
}
