import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FriendTransactionsPage } from './friend-transactions.page';

describe('FriendTransactionsPage', () => {
  let component: FriendTransactionsPage;
  let fixture: ComponentFixture<FriendTransactionsPage>;

  beforeEach(async () => {
    fixture = TestBed.createComponent(FriendTransactionsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
