import { Component, inject, OnInit } from '@angular/core';
import {
  IonBackButton,
  IonButtons,
  IonContent,
  IonHeader, IonLabel,
  IonList,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { Friend } from '../model';
import { NavParams } from '@ionic/angular';

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
  ],
})
export class TransactionDetailsComponent  implements OnInit {
  readonly transaction: Friend = inject(NavParams).data?.['transaction'];
  constructor() { }

  ngOnInit() {}

}
