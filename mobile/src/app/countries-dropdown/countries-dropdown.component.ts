import { Component, computed, forwardRef, inject, OnInit, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FakeDropdownComponent } from '../fake-dropdown/fake-dropdown.component';
import { COUNTRIES_WITH_CALLING_CODES, Country } from '../constants';
import { SelectModalComponent } from '../currencies-modal/select-modal.component';
import { ModalService } from '../modal.service';

@Component({
  selector: 'mr-countries-dropdown',
  templateUrl: './countries-dropdown.component.html',
  styleUrls: ['./countries-dropdown.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CountriesDropdownComponent),
    multi: true
  }],
  imports: [
    FakeDropdownComponent,
  ],
})
export class CountriesDropdownComponent  implements ControlValueAccessor {
  modalService = inject(ModalService);
  value = signal<string|null>(null);
  isDisabled = signal<boolean>(false);
  countries = COUNTRIES_WITH_CALLING_CODES;
  selectedCountryCode = computed(() =>
    this.value()
    || this.countries[0].code);
  selectedCountry = computed(() => this.countries.find(
    country => country.code === this.selectedCountryCode()
  ));
  selectedCountryText = computed(() => this.selectedCountry() ?
    `${this.selectedCountry()!.flag} ${this.selectedCountry()!.dialCode}` :
    ''
  );

  private onChange = (code: string) => {};
  private onTouched = () => {};

  constructor() { }

  writeValue(countryCode: string): void {
    this.value.set(countryCode);
  }
  registerOnChange(fn: typeof this.onChange): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
  setDisabledState?(isDisabled: boolean): void {
    this.isDisabled.set(isDisabled);
  }

  async chooseCountry() {
    this.onTouched();
    const optionLabel = (country: Country) => `${country.flag} ${country.name} (${country.dialCode})`;
    const modalIndex = await this.modalService.showModal({
      component: SelectModalComponent,
      componentProps: {
        items: COUNTRIES_WITH_CALLING_CODES.map((currency) => ({...currency, optionLabel: optionLabel(currency)})),
        selectedItem: {
          ...this.selectedCountry(),
          optionLabel: optionLabel(this.selectedCountry())
        },
        mostlyUsedItems: []
      },
      handleBehavior: 'cycle',
      initialBreakpoint: 0.5,
      breakpoints: [0.25, 0.5, 0.75]
    })
    await this.modalService.onWillDismiss<Country>(modalIndex)
      .then((value) => {
        if (value.role === 'confirm') {
          this.value.set(value.data.code)
          this.onChange(value.data.code);
        }
      })
  }
}
