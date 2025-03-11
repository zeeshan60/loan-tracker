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
  phoneNumber?: string;
  email: string
}

export interface FriendWithBalance extends Friend {
  mainBalance: { amount: number, currency: string, isOwed: boolean };
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
  updatedBy?: {
    name: string,
    id: string,
  },
  description: string,
  transactionId: string,
  totalAmount: number,
  splitType: SplitOptions,
  friend: Friend,
  amountResponse: {
    amount: number,
    currency: string,
    isOwed: true
  },
  history: {
    changedBy: string,
    changedByName: string,
    changedByPhoto: string,
    changes: {
      oldValue: string,
      newValue: string,
      type: HistoryChangeType
    }[]
  }[],
  deleted: boolean,
}

export interface TransactionsByMonth {
  date: string;
  transactions: Transaction[]
}
