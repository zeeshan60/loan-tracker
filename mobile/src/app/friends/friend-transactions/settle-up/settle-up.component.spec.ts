import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { SettleUpComponent } from './settle-up.component';

describe('SettleUpComponent', () => {
  let component: SettleUpComponent;
  let fixture: ComponentFixture<SettleUpComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [SettleUpComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SettleUpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
