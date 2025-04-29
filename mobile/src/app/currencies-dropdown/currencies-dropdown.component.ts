import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FakeDropdownComponent } from '../fake-dropdown/fake-dropdown.component';
import { SelectModalComponent } from '../currencies-modal/select-modal.component';
import { CURRENCIES, Currency } from '../constants';
import { ModalService } from '../modal.service';
import { AuthStore } from '../login/auth.store';
import { FriendsStore } from '../friends/friends.store';

@Component({
  selector: 'app-currencies-dropdown',
  templateUrl: './currencies-dropdown.component.html',
  styleUrls: ['./currencies-dropdown.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FakeDropdownComponent,
  ],
})
export class CurrenciesDropdownComponent {
  label = input<string>();
  selectedCurrency = input<Currency>();
  selected = output<any>();
  modalService = inject(ModalService);
  authStore = inject(AuthStore);
  friendsStore = inject(FriendsStore);
  constructor() {
  }

  async chooseCurrency() {
    const optionLabel = (option: Currency) => `${option.code} ${option.name} (${option.symbol})`
    const modalIndex = await this.modalService.showModal({
      component: SelectModalComponent,
      componentProps: {
        items: CURRENCIES.map((currency) => ({...currency, optionLabel: optionLabel(currency)})),
        selectedItem: {
          ...this.selectedCurrency(),
          optionLabel: optionLabel(this.selectedCurrency())
        },
        mostlyUsedItems: (() => {
          const defaultCurrencyCode = this.authStore.user().currency;
          return CURRENCIES.filter(currency => [
            defaultCurrencyCode,
            ...this.friendsStore.mostlyUsedCurrencies()
          ].includes(currency.code))
           .map(currency => ({
             ...currency,
             optionLabel: optionLabel(currency)
           }))
        })()
      },
      handleBehavior: 'cycle',
      initialBreakpoint: 0.5,
      breakpoints: [0.25, 0.5, 0.75]
    })
    await this.modalService.onWillDismiss<Currency>(modalIndex)
      .then((value) => {
        if (value.role === 'confirm') {
          this.selected.emit(value.data.code)
        }
      })
  }
}
