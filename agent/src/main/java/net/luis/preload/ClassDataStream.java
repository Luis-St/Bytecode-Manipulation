package net.luis.preload;

import net.luis.preload.data.ClassContent;
import net.luis.preload.data.ClassInfo;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 *
 * @author Luis-St
 *
 */

public class ClassDataStream {
	
	private final Stream<Map.Entry<ClassInfo, ClassContent>> stream;
	
	ClassDataStream(Stream<Map.Entry<ClassInfo, ClassContent>> stream) {
		this.stream = stream;
	}
	
	//region Helper methods
	private Stream<ClassInfo> asInfoStream() {
		return this.stream.map(Map.Entry::getKey);
	}
	
	private Stream<ClassContent> asContentStream() {
		return this.stream.map(Map.Entry::getValue);
	}
	//endregion
	
	//region Filter
	public ClassDataStream filter(ClassDataPredicate predicate) {
		return new ClassDataStream(this.stream.filter(entry -> predicate.test(entry.getKey(), entry.getValue())));
	}
	
	public Stream<ClassInfo> filterInfo(Predicate<ClassInfo> predicate) {
		return this.asInfoStream().filter(predicate);
	}
	
	public Stream<ClassContent> filterContent(Predicate<ClassContent> predicate) {
		return this.asContentStream().filter(predicate);
	}
	//endregion
	
	//region Map
	public <R> Stream<R> map(BiFunction<ClassInfo, ClassContent, ? extends R> mapper) {
		return this.stream.map(entry -> mapper.apply(entry.getKey(), entry.getValue()));
	}
	
	public <R> Stream<R> mapInfo(Function<ClassInfo, ? extends R> mapper) {
		return this.stream.map(entry -> mapper.apply(entry.getKey()));
	}
	
	public <R> Stream<R> mapContent(Function<ClassContent, ? extends R> mapper) {
		return this.stream.map(entry -> mapper.apply(entry.getValue()));
	}
	//endregion
	
	public ClassDataStream peek(BiConsumer<ClassInfo, ClassContent> action) {
		return new ClassDataStream(this.stream.peek(entry -> action.accept(entry.getKey(), entry.getValue())));
	}
	
	public ClassDataStream limit(long maxSize) {
		return new ClassDataStream(this.stream.limit(maxSize));
	}
	
	public ClassDataStream skip(long n) {
		return new ClassDataStream(this.stream.skip(n));
	}
	
	public long count() {
		return this.stream.count();
	}
	
	//region Match
	public boolean anyInfoMatch(Predicate<ClassInfo> predicate) {
		return this.stream.anyMatch(entry -> predicate.test(entry.getKey()));
	}
	
	public boolean allInfoMatch(Predicate<ClassInfo> predicate) {
		return this.stream.allMatch(entry -> predicate.test(entry.getKey()));
	}
	
	public boolean noneInfoMatch(Predicate<ClassInfo> predicate) {
		return this.stream.noneMatch(entry -> predicate.test(entry.getKey()));
	}
	
	public boolean anyContentMatch(Predicate<ClassContent> predicate) {
		return this.stream.anyMatch(entry -> predicate.test(entry.getValue()));
	}
	
	public boolean allContentMatch(Predicate<ClassContent> predicate) {
		return this.stream.allMatch(entry -> predicate.test(entry.getValue()));
	}
	
	public boolean noneContentMatch(Predicate<ClassContent> predicate) {
		return this.stream.noneMatch(entry -> predicate.test(entry.getValue()));
	}
	//endregion
	
	//region Find
	public Optional<ClassInfo> findFirstInfo() {
		return this.asInfoStream().findFirst();
	}
	
	public Optional<ClassInfo> findAnyInfo() {
		return this.asInfoStream().findAny();
	}
	
	public Optional<ClassContent> findFirstContent() {
		return this.asContentStream().findFirst();
	}
	
	public Optional<ClassContent> findAnyContent() {
		return this.asContentStream().findAny();
	}
	//endregion
	
	//region Convert
	public Iterator<ClassInfo> infoIterator() {
		return this.asInfoStream().iterator();
	}
	
	public Iterator<ClassContent> contentIterator() {
		return this.asContentStream().iterator();
	}
	
	public ClassInfo[] toInfoArray() {
		return this.asInfoStream().toArray(ClassInfo[]::new);
	}
	
	public ClassContent[] toContentArray() {
		return this.asContentStream().toArray(ClassContent[]::new);
	}
	
	public List<ClassInfo> toInfoList() {
		return this.asInfoStream().toList();
	}
	
	public List<ClassContent> toContentList() {
		return this.asContentStream().toList();
	}
	//endregion
	
	//region Collect
	public <R, A> R collectInfo(Collector<ClassInfo, A, R> collector) {
		return this.asInfoStream().collect(collector);
	}
	
	public <R> R collectInfo(Supplier<R> supplier, BiConsumer<R, ClassInfo> accumulator, BiConsumer<R, R> combiner) {
		return this.asInfoStream().collect(supplier, accumulator, combiner);
	}
	
	public <R, A> R collectContent(Collector<ClassContent, A, R> collector) {
		return this.asContentStream().collect(collector);
	}
	
	public <R> R collectContent(Supplier<R> supplier, BiConsumer<R, ClassContent> accumulator, BiConsumer<R, R> combiner) {
		return this.asContentStream().collect(supplier, accumulator, combiner);
	}
	//endregion
	
	public void forEach(BiConsumer<ClassInfo, ClassContent> action) {
		this.stream.forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
	}
}
