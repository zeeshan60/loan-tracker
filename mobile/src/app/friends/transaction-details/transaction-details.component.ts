import { Component, inject, OnInit } from '@angular/core';
import {
  IonAvatar,
  IonBackButton, IonButton,
  IonButtons,
  IonContent,
  IonHeader, IonIcon, IonItem, IonLabel,
  IonList,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { Friend, Transaction } from '../model';
import { NavParams } from '@ionic/angular';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';

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
  readonly transaction: Transaction = inject(NavParams).data?.['transaction'];
  constructor() {
    console.log(this.transaction);
  }

  ngOnInit() {}

}
