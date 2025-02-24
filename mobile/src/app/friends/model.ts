import { SplitOptions } from '../define-expense/define-expense.component';

export interface Friend {
  friendId: string;
  photoUrl: string|null;
  name: string;
  mainBalance: { amount: number, isOwed: boolean } | null;
  otherBalances: { amount: number, isOwed: boolean }[]
}

export interface Transaction {
  date: string,
  description: string,
  transactionId: string,
  totalAmount: number,
  splitType: SplitOptions,
  friendName: string,
  amountResponse: {
    amount: number,
    currency: string,
    isOwed: true
  },
  "history": {
    amount: number,
    currency: string,
    isOwed: true
  }[]
}

export interface TransactionsByMonth {
  date: string;
  transactions: Transaction[]
}
