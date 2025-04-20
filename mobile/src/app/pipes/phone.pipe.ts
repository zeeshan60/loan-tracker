import { Pipe, PipeTransform } from '@angular/core';
import parsePhoneNumber, { CountryCode } from 'libphonenumber-js';

@Pipe({
  standalone: true,
  name: 'phone',
})
export class PhonePipe implements PipeTransform {

  transform(phoneNumber: string|null = '', ...args: unknown[]): unknown {
    if (phoneNumber) {
      const phoneNumberInter = parsePhoneNumber(phoneNumber);
      return phoneNumberInter?.formatInternational();
    }
    return 'N/A';
  }

}
