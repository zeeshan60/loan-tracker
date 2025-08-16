export interface Group {
  friendId: string;
  photoUrl: string|null;
  name: string;
  phone?: string;
  email: string
}

export type Balance = {
  currency: string,
  amount: number,
  isOwed: boolean
}

export type OtherBalance = {
  convertedAmount: Balance,
  amount: Balance,
}

export interface GroupWithBalance extends Group {
  mainBalance?: { amount: number, currency: string, isOwed: boolean };
  otherBalances?: OtherBalance[]
}
