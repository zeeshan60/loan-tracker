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
  IonLabel, IonList, IonSpinner, IonTitle, IonToolbar,
} from '@ionic/angular/standalone';
import { NavParams } from '@ionic/angular';
import { Friend } from '../model';

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

  constructor() {
    console.log(this.friend);
  }

  ngOnInit() {}

  ionViewDidEnter() {
    console.log('Transactions page did enter');
  }

  ionViewDidLeave() {
    console.log('Transactions page did leave');
  }

  ionTabsDidChange(event: any) {
    console.log('tabs changed..');
  }
}
