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
import { FriendsStore } from '../friends/friends.store';
import { AuthStore } from '../login/auth.store';

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
  friendsStore = inject(FriendsStore);
  authStore = inject(AuthStore);
  private filteredCurrencies = computed(() => {
    return CURRENCIES.filter((currency) => `${currency.code} ${currency.name} ${currency.symbol}`.toLowerCase().includes(this.filter().toLowerCase()))
  });
  mostlyUsedCurrencies = computed(() => {
    const defaultCurrencyCode = this.authStore.user().currency;
    const selectedCurrencyCode = this.selectedCurrencyCode();
    return this.filteredCurrencies().filter(currency => [
      defaultCurrencyCode,
      selectedCurrencyCode,
      ...this.friendsStore.mostlyUsedCurrencies()
    ].includes(currency.code))
  });
  unUsedCurrencies = computed(() => {
    const usedCurrencies = this.mostlyUsedCurrencies().map(currency => currency.code)
    return this.filteredCurrencies().filter(currency => !usedCurrencies.includes(currency.code))
  });
  modalService = inject(ModalService);
  constructor() { }

  ngOnInit() {
  }

  selectCurrency(currency: Currency) {
    this.modalService.dismiss(this.modalIndex(), currency, 'confirm');
  }
}
