import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { COUNTRIES_WITH_CALLING_CODES, Country, CURRENCIES, Currency, PHONE_MASKS } from '../constants';
import {
  AbstractControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { IonInput, IonSelect, IonSelectOption } from '@ionic/angular/standalone';
import { NgForOf } from '@angular/common';
import { isPhoneNumberValid, toInternationalPhone } from '../utility-functions';
import { MaskitoElementPredicate } from '@maskito/core';
import { MaskitoDirective } from '@maskito/angular';
import { CurrenciesDropdownComponent } from '../currencies-dropdown/currencies-dropdown.component';
import { SelectModalComponent } from '../currencies-modal/select-modal.component';
import { FakeDropdownComponent } from '../fake-dropdown/fake-dropdown.component';
import { ModalService } from '../modal.service';

@Component({
  selector: 'app-phone-with-country',
  templateUrl: './phone-with-country.component.html',
  styleUrls: ['./phone-with-country.component.scss'],
  standalone: true,
  imports: [
    FormsModule,
    IonInput,
    IonSelect,
    IonSelectOption,
    NgForOf,
    ReactiveFormsModule,
    MaskitoDirective,
    CurrenciesDropdownComponent,
    SelectModalComponent,
    FakeDropdownComponent,
  ],
})
export class PhoneWithCountryComponent  implements OnInit {
  readonly group = input<FormGroup>();
  readonly phoneNumberValidator = (): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!value || !this.group()) {
        return null;
      }

      return isPhoneNumberValid(toInternationalPhone(value, this.group()!.get('country')!.value))
        ? null
        : { invalidPhone: true };
    };
  }
  readonly phoneMasks = PHONE_MASKS;
  readonly maskPredicate: MaskitoElementPredicate = async (el) => (el as HTMLIonInputElement).getInputElement();
  countries = COUNTRIES_WITH_CALLING_CODES;
  selectedCountryCodeChanges = signal('');
  selectedCountryCode = computed(() =>
    this.selectedCountryCodeChanges()
    || this.group()?.get('country')!.value
    || this.countries[0].code);
  selectedCountry = computed(() => this.countries.find(
    country => country.code === this.selectedCountryCode()
  ));
  selectedCountryText = computed(() => this.selectedCountry() ?
    `${this.selectedCountry()!.flag} ${this.selectedCountry()!.dialCode}` :
    ''
  );
  phoneMask = computed(() => this.selectedCountryCode() ? this.phoneMasks[this.selectedCountryCode()] : {});
  selectedCountryPlaceholder = computed(() => this.selectedCountry()?.placeholder || '');

  modalService = inject(ModalService);

  constructor() { }

  getErrorTextForPhone() {
    if (this.group()?.controls['phoneNumber'].hasError('invalidPhone')) {
      return 'Invalid phone number.'
    } else if (this.group()?.controls['phoneNumber'].hasError('required')) {
      return 'Phone is required'
    } else {
      return 'Invalid';
    }
  }

  ngOnInit() {
    this.group()!.get('country')!.valueChanges.subscribe((value) => {
      this.selectedCountryCodeChanges.set(value);
      this.group()!.get('phoneNumber')?.setValue('');
      this.group()!.get('phoneNumber')?.updateValueAndValidity();
    });

    const phoneNumberControl = this.group()!.get('phoneNumber');
    if (phoneNumberControl) {
      phoneNumberControl.addValidators([
        this.phoneNumberValidator()
      ]);
      phoneNumberControl.updateValueAndValidity();
    }
  }

  async chooseCountry() {
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
          this.group()!.get('country').setValue(value.data.code)
        }
      })

  }
}
