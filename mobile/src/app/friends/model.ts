export interface Friend {
  photoUrl: string|null,
  name: string,
  loanAmount: { amount: number, isOwed: boolean } | null
}
