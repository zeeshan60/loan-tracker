import parsePhoneNumber, {CountryCode} from 'libphonenumber-js';

export function shortName(name: string): string {
  const parts = name.trim().split(" ");
  if (parts.length === 1) return name;
  return `${parts[0]} ${parts[1][0]}`;
}

export function toNationalPhone(internationalPhone: string) {
  const phoneNumberInter = parsePhoneNumber(internationalPhone);
  return phoneNumberInter?.formatNational();
}

export function toInternationalPhone(nationalPhone: string, countryCode: string) {
  const phoneNumberInter = parsePhoneNumber(nationalPhone, countryCode as CountryCode);
  return phoneNumberInter?.formatInternational() || '';
}

export function extractCountryCode(internationalPhone: string) {
  const phoneNumberInter = parsePhoneNumber(internationalPhone);
  return phoneNumberInter?.country;
}

export function isValidPhoneNumber(internationalPhone: string) {
  const phoneNumberInter = parsePhoneNumber(internationalPhone);
  return phoneNumberInter?.isValid();
}
