import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { FriendTransactionsComponent } from './friend-transactions.component';

describe('FriendTransactionsComponent', () => {
  let component: FriendTransactionsComponent;
  let fixture: ComponentFixture<FriendTransactionsComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [FriendTransactionsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FriendTransactionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
