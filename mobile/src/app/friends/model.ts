import { SplitOptions } from '../define-expense/define-expense.component';

export interface Friend {
  friendId: string;
  photoUrl: string|null;
  name: string;
  mainBalance: { amount: number, currency: string, isOwed: boolean } | null;
  otherBalances: { amount: number, currency: string, isOwed: boolean }[]
}

export interface Transaction {
  date: string,
  createdAt: string,
  updatedAt: string,
  createdBy: {
    name: string,
    id: string,
  },
  updatedBy: {
    name: string,
    id: string,
  },
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
