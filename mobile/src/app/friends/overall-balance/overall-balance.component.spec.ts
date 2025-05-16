import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { OverallBalanceComponent } from './overall-balance.component';

describe('OverallBalanceComponent', () => {
  let component: OverallBalanceComponent;
  let fixture: ComponentFixture<OverallBalanceComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
    imports: [IonicModule.forRoot(), OverallBalanceComponent]
}).compileComponents();

    fixture = TestBed.createComponent(OverallBalanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
