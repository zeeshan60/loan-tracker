import { ChangeDetectionStrategy, Component, computed, Input, OnInit } from '@angular/core';
import { Balance } from '../friends.store';
import { IonAccordion, IonAccordionGroup, IonItem, IonLabel } from '@ionic/angular/standalone';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-overall-balance',
  templateUrl: './overall-balance.component.html',
  styleUrls: ['./overall-balance.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonItem,
    IonLabel,
    IonAccordionGroup,
    IonAccordion,
    CurrencyPipe,
  ],
})
export class OverallBalanceComponent implements OnInit {

  @Input() overallBalance?: {
    main: Balance,
    other: Balance[]
  } | null = null;

  readonly sortedOtherBalance = computed(() => this.overallBalance?.other
    .sort((a, b) => b.amount - a.amount) || []);
  readonly hasDifferentCurrencies = computed(() => {
    return this.sortedOtherBalance()
      .some((balance) => balance.currency !== this.overallBalance?.main.currency);
  });

  constructor() {
  }

  ngOnInit() {
  }

}
