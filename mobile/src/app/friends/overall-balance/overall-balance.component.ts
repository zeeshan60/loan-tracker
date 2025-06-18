import { ChangeDetectionStrategy, Component, computed, input, Input, OnInit, output, Signal } from '@angular/core';
import { OverallBalance } from '../friends.store';
import {
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
} from '@ionic/angular/standalone';
import { CurrencyPipe, NgClass } from '@angular/common';

@Component({
  selector: 'mr-overall-balance',
  templateUrl: './overall-balance.component.html',
  styleUrls: ['./overall-balance.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonItem,
    IonLabel,
    CurrencyPipe,
    IonList,
    NgClass,
    IonIcon,
  ],
})
export class OverallBalanceComponent implements OnInit {

  overallBalance = input<OverallBalance>();

  settleUp = output();

  readonly sortedOtherBalance = computed(() => this.overallBalance()?.other
    .sort((a, b) => b.amount.amount - a.amount.amount) || []);

  readonly hasDifferentCurrencies = computed(() => {
    return this.sortedOtherBalance()
      .some((balance) => balance.amount.currency !== this.overallBalance().main.currency);
  });

  constructor() {
  }

  ngOnInit() {
  }

}
