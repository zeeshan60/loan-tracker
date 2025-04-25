import { ChangeDetectionStrategy, Component, computed, inject, input, model, OnInit } from '@angular/core';
import {
  IonContent, IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonSearchbar,
} from '@ionic/angular/standalone';
import { ModalIndex, ModalService } from '../modal.service';
import { FormsModule } from '@angular/forms';
import { CURRENCIES, Currency } from '../constants';

@Component({
  selector: 'app-currencies-modal',
  templateUrl: './currencies-modal.component.html',
  styleUrls: ['./currencies-modal.component.scss'],
  standalone: true,
  imports: [
    IonContent,
    IonSearchbar,
    IonList,
    IonItem,
    IonLabel,
    FormsModule,
    IonIcon,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CurrenciesModalComponent  implements OnInit {
  modalIndex = input.required<ModalIndex>()
  selectedCurrencyCode = input<string>();
  filter = model<string>('');
  filteredCurrencies = computed(() => {
    return CURRENCIES.filter((currency) => `${currency.code} ${currency.name} ${currency.symbol}`.toLowerCase().includes(this.filter().toLowerCase()))
  });
  modalService = inject(ModalService);
  constructor() { }

  ngOnInit() {
  }

  selectCurrency(currency: Currency) {
    this.modalService.dismiss(this.modalIndex(), currency, 'confirm');
  }
}
