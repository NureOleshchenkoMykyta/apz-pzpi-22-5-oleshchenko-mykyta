interface State {
handle(): void;
}

class Context {
state: State;

constructor(initialState: State) {
this.state = initialState;
}

setState(state: State) {
this.state = state;
}

handle() {
this.state.handle();
}
}

class ConcreteStateA implements State {
handle() {
console.log("ConcreteStateA");
}
}

class ConcreteStateB implements State {
handle() {
console.log("ConcreteStateB");
}
}

 const context = new Context(new ConcreteStateA());
context.handle();
context.setState(new ConcreteStateB());
context.handle();
