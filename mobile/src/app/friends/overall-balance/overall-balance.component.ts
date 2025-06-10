import { ChangeDetectionStrategy, Component, computed, input, Input, OnInit, output, Signal } from '@angular/core';
import { Balance } from '../friends.store';
import { IonAccordion, IonAccordionGroup, IonItem, IonLabel, IonList, IonButton, IonButtons, IonIcon } from '@ionic/angular/standalone';
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
    IonButton,
    IonButtons,
    IonIcon,
    NgClass,
  ],
})
export class OverallBalanceComponent implements OnInit {

  overallBalance = input<{
    main: Balance,
    other: Balance[]
  } | null>();

  settleUp = output();

  readonly sortedOtherBalance = computed(() => this.overallBalance()?.other
    .sort((a, b) => b.amount - a.amount) || []);

  readonly hasDifferentCurrencies = computed(() => {
    return this.sortedOtherBalance()
      .some((balance) => balance.currency !== this.overallBalance().main.currency);
  });

  constructor() {
  }

  ngOnInit() {
  }

}
