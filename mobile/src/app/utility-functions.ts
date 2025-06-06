import parsePhoneNumber, {CountryCode} from 'libphonenumber-js';

export function shortName(name: string): string {
  const parts = name.trim().split(" ");
  if (parts.length === 1) return name;
  return `${parts[0]} ${parts[1][0]}`;
}

export function toNationalPhone(internationalPhone: string) {
  if (!internationalPhone) {
    return '';
  }
  const phoneNumberInter = parsePhoneNumber(internationalPhone);
  return phoneNumberInter?.formatNational();
}

export function toInternationalPhone(nationalPhone: string, countryCode: string) {
  const phoneNumberInter = parsePhoneNumber(nationalPhone, countryCode as CountryCode);
  return phoneNumberInter?.number || '';
}

export function extractCountryCode(internationalPhone?: string) {
  if (!internationalPhone) {
    return '';
  }
  const phoneNumberInter = parsePhoneNumber(internationalPhone);
  return phoneNumberInter?.country;
}

export function isPhoneNumberValid(internationalPhone: string) {
  if (!internationalPhone) {
    return false;
  }
  const phoneNumberInter = parsePhoneNumber(internationalPhone);
  return phoneNumberInter?.isValid();
}

export const extractFirebaseErrorMessage = (errorCode: string): string => {
  const cleaned = errorCode.replace(/[()]/g, '');
  const parts = cleaned.split('/');
  const message = parts[1] || '';
  return message.replace(/-/g, ' ');
};
