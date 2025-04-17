import { environment } from '../environments/environment';

export const DEFAULT_TOAST_DURATION = 2000;
export const PRIVATE_API = `${environment.apiBaseUrl}/api/v1`;
export const PUBLIC_API = environment.apiBaseUrl;
export const CURRENCIES = [ 'PKR', 'USD', 'SGD', 'THB'];
export const PHONE_MASKS: { [key: string]: any } = {
  'PK': {
    mask: [
      '0', '3', /\d/, /\d/, ' ', /\d/, /\d/, /\d/, /\d/, /\d/, /\d/, /\d/
    ],
    autocomplete: 'tel',
    overwriteMode: 'shift',
  },
  'IN': {
    mask: [
      /[9|8|7]/, /\d/, /\d/, /\d/, /\d/, ' ', /\d/, /\d/, /\d/, /\d/, /\d/,
    ],
    autocomplete: 'tel',
    overwriteMode: 'shift',
  },
  'US': {
    mask: [
      '(', /\d/, /\d/, /\d/, ')', ' ', /\d/, /\d/, /\d/, '-', /\d/, /\d/, /\d/, /\d/
    ],
    autocomplete: 'tel',
    overwriteMode: 'shift',
  },
  'GB': {
    mask: [
      '0', '7', /\d/, /\d/, /\d/, ' ', /\d/, /\d/, /\d/, /\d/, /\d/, /\d/
    ],
    autocomplete: 'tel',
    overwriteMode: 'shift',
  },
  'TH': {
    mask: [
      '0', '8', /\d/, ' ', /\d/, /\d/, /\d/, ' ', /\d/, /\d/, /\d/, /\d/
    ],
    autocomplete: 'tel',
    overwriteMode: 'shift',
  },
  'SG': {
    mask: [
      '9', /\d/, /\d/, /\d/, ' ', /\d/, /\d/, /\d/, /\d/
    ],
    autocomplete: 'tel',
    overwriteMode: 'shift',
  },
  'CN': {
    mask: [
      '1', /\d/, /\d/, ' ', /\d/, /\d/, /\d/, /\d/, ' ', /\d/, /\d/, /\d/, /\d/
    ],
    autocomplete: 'tel',
    overwriteMode: 'shift',
  }
};
export const COUNTRIES_WITH_CALLING_CODES = [
  {
    name: 'Pakistan',
    code: 'PK',
    dialCode: '+92',
    flag: '🇵🇰',
    placeholder: '0313 1245852'
  },
  {
    name: 'India',
    code: 'IN',
    dialCode: '+91',
    flag: '🇮🇳',
    placeholder: '98765 43210'
  },
  {
    name: 'United States',
    code: 'US',
    dialCode: '+1',
    flag: '🇺🇸',
    placeholder: '(123) 456-7890'
  },
  {
    name: 'United Kingdom',
    code: 'GB',
    dialCode: '+44',
    flag: '🇬🇧',
    placeholder: '07123 456789'
  },
  {
    name: 'Thailand',
    code: 'TH',
    dialCode: '+66',
    flag: '🇹🇭',
    placeholder: '081 234 5678'
  },
  {
    name: 'Singapore',
    code: 'SG',
    dialCode: '+65',
    flag: '🇸🇬',
    placeholder: '9123 4567'
  },
  {
    name: 'China',
    code: 'CN',
    dialCode: '+86',
    flag: '🇨🇳',
    placeholder: '138 1234 5678'
  }
];
