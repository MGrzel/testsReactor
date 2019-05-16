package edu.iis.mto.testreactor.exc3;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class AtmMachineTest {

    private CardProviderService cardProviderService;
    private BankService bankService;
    private MoneyDepot moneyDepot;
    private AtmMachine atmMachine;

    @Before
    public void init() {
        cardProviderService = Mockito.mock(CardProviderService.class);
        bankService = Mockito.mock(BankService.class);
        moneyDepot = Mockito.mock(MoneyDepot.class);

        atmMachine = new AtmMachine(cardProviderService, bankService, moneyDepot);
    }

    @Test
    public void itCompiles() {
        assertThat(true, equalTo(true));
    }

    @Test(expected = CardAuthorizationException.class)
    public void shouldThrowCardAuthorizationExceptionWhenAuthCodeNotPresent() {
        Money money = Money.builder()
                .withAmount(10)
                .withCurrency(Currency.PL)
                .build();

        Card card = Card.builder()
                .withCardNumber("test")
                .withPinNumber(1111)
                .build();

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.ofNullable(null));

        atmMachine.withdraw(money, card);
    }

    @Test(expected = WrongMoneyAmountException.class)
    public void shouldThrowWrongMoneyAmountExceptionWhenMoneyAmountIsLessOrEqualZero() {
        Money money = Money.builder()
                .withAmount(0)
                .withCurrency(Currency.PL)
                .build();

        Card card = Card.builder()
                .withCardNumber("test")
                .withPinNumber(1111)
                .build();

        atmMachine.withdraw(money, card);
    }

    @Test(expected = WrongMoneyAmountException.class)
    public void shouldThrowWrongMoneyAmountExceptionWhenMoneyAmountCannotBePayedWithBanknotes() {
        Money money = Money.builder()
                .withAmount(3)
                .withCurrency(Currency.PL)
                .build();

        Card card = Card.builder()
                .withCardNumber("test")
                .withPinNumber(1111)
                .build();

        atmMachine.withdraw(money, card);
    }
}
