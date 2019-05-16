package edu.iis.mto.testreactor.exc3;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
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

    @Test(expected = InsufficientFundsException.class)
    public void shouldThrowInsufficientFundsExceptionWhenChargeReturnsFalse() {
        Money money = Money.builder()
                .withAmount(10)
                .withCurrency(Currency.PL)
                .build();

        Card card = Card.builder()
                .withCardNumber("test")
                .withPinNumber(1111)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(false);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(AuthenticationToken.builder()
                        .withAuthorizationCode(1111)
                        .withUserId("1")
                        .build()));

        atmMachine.withdraw(money, card);
    }

    @Test(expected = MoneyDepotException.class)
    public void shouldThrowMoneyDepotExceptionWhenMoneyDepotFailToReleaseMoney() {
        Money money = Money.builder()
                .withAmount(10)
                .withCurrency(Currency.PL)
                .build();

        Card card = Card.builder()
                .withCardNumber("test")
                .withPinNumber(1111)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(AuthenticationToken.builder()
                        .withAuthorizationCode(1111)
                        .withUserId("1")
                        .build()));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(false);

        atmMachine.withdraw(money, card);
    }
}
