import { SplitOptions } from '../define-expense/define-expense.component';

export enum HistoryChangeType {
  DESCRIPTION = "DESCRIPTION",
  TOTAL_AMOUNT = "TOTAL_AMOUNT",
  CURRENCY = "CURRENCY",
  SPLIT_TYPE = "SPLIT_TYPE",
  DELETED = "DELETED",
  TRANSACTION_DATE = "TRANSACTION_DATE"
}
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
  history: {
    changedBy: string,
    changes: {
      oldValue: string,
      newValue: string,
      type: HistoryChangeType
    }[]
  }[]
}

export interface TransactionsByMonth {
  date: string;
  transactions: Transaction[]
}
