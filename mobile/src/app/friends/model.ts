export interface Friend {
  photoUrl: string,
  name: string,
  loanAmount: { amount: number, isOwed: boolean } | null
}
